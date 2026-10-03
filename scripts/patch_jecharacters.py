"""Build and regression-test a local repair of JECharacters Fabric 26.1.2 / 4.6.7.

Does not edit the input mod or any Minecraft instance. Uses cached compiler dependencies.
"""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import argparse
import hashlib
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'scripts/compat/jecharacters'
HOOK = 'me/towdium/jecharacters/mixin/MixinTransformerHook.class'
ORIGINAL_HOOK_SHA256 = '801a07615f9083f40ae01b857adf63fefb0842cbae7a1e83d1ce404917d3cad4'
PATCH_VERSION = '4.6.7+threadfix.1'
TEST_MAIN = 'me.towdium.jecharacters.mixin.MixinTransformerHookTest'


def cached_library(cache, group, artifact):
    directory = cache / 'modules-2/files-2.1' / group / artifact
    candidates = list(directory.glob(f'*/*/{artifact}-*.jar'))
    candidates = [p for p in candidates if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
    if not candidates:
        raise RuntimeError(f'Missing cached {group}:{artifact} in {cache}')
    return max(candidates, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.parent.parent.name)))


def run(args, *, expect_failure=False):
    result = subprocess.run(args, capture_output=True, text=True, timeout=60)
    output = result.stdout + result.stderr
    if expect_failure:
        if result.returncode == 0 or 'Non-target early return must not leak' not in output:
            raise RuntimeError('Original hook did not reproduce the expected regression:\n' + output)
        print('Original JECharacters hook reproduced the non-target stack leak.')
    else:
        if result.returncode:
            raise RuntimeError(output)
        if output.strip():
            print(output.strip())
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path, help='Original jecharacters-26.1.2-fabric-4.6.7.jar')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/compat/jecharacters-26.1.2-fabric-4.6.7-threadfix.jar')
    parser.add_argument('--gradle-cache', type=Path, default=Path.home() / '.gradle/caches')
    args = parser.parse_args()
    original = args.input.resolve()
    output = args.output.resolve()
    if output == original:
        raise RuntimeError('Output must differ from the original mod path')
    with ZipFile(original) as source:
        meta = json.loads(source.read('fabric.mod.json'))
        if meta['id'] != 'jecharacters' or meta['version'] != '4.6.7':
            raise RuntimeError('This repair only supports the original JECharacters 4.6.7 Fabric build')
        if hashlib.sha256(source.read(HOOK)).hexdigest() != ORIGINAL_HOOK_SHA256:
            raise RuntimeError('Unrecognized hook bytecode; refusing to patch a different implementation')

    dependencies = [cached_library(args.gradle_cache, group, artifact) for group, artifact in (
        ('net.fabricmc', 'sponge-mixin'), ('org.ow2.asm', 'asm'), ('org.ow2.asm', 'asm-tree'),
        ('com.google.code.gson', 'gson'), ('org.apache.logging.log4j', 'log4j-api'),
        ('org.apache.logging.log4j', 'log4j-core'),
    )]
    work = ROOT / 'build/compat/jecharacters'
    classes = work / 'classes'; tests = work / 'tests'
    classes.mkdir(parents=True, exist_ok=True); tests.mkdir(parents=True, exist_ok=True)
    base_cp = os.pathsep.join(map(str, [original, *dependencies]))
    fixed_cp = os.pathsep.join([str(classes), base_cp])
    run(['javac', '-proc:none', '--release', '25', '-cp', base_cp, '-d', str(classes), str(SOURCE / 'MixinTransformerHook.java')])
    run(['javac', '-proc:none', '--release', '25', '-cp', fixed_cp, '-d', str(tests), str(SOURCE / 'MixinTransformerHookTest.java')])
    run(['java', '-cp', os.pathsep.join([str(tests), base_cp]), TEST_MAIN], expect_failure=True)
    test_log = run(['java', '-cp', os.pathsep.join([str(tests), fixed_cp]), TEST_MAIN])
    hook_bytes = (classes / HOOK).read_bytes()
    meta['version'] = PATCH_VERSION
    meta.setdefault('custom', {})['jecharacters:local_thread_fix'] = 1
    manifest = {
        'upstream': 'https://github.com/Towdium/JustEnoughCharacters',
        'original_jar_sha256': hashlib.sha256(original.read_bytes()).hexdigest(),
        'original_hook_sha256': ORIGINAL_HOOK_SHA256,
        'replacement_hook_sha256': hashlib.sha256(hook_bytes).hexdigest(),
        'version': PATCH_VERSION,
        'changes': ['Thread-local transformation stack', 'Finally cleanup for all return and exception paths'],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(original) as source, ZipFile(output, 'w', compression=ZIP_DEFLATED) as target:
        for info in source.infolist():
            data = source.read(info.filename)
            if info.filename == HOOK:
                data = hook_bytes
            elif info.filename == 'fabric.mod.json':
                data = json.dumps(meta, ensure_ascii=False, indent=2).encode()
            target.writestr(info, data)
        target.writestr('LICENSE-JECHARACTERS', (SOURCE / 'LICENSE').read_bytes())
        target.writestr('META-INF/jecharacters-threadfix.json', json.dumps(manifest, indent=2))
    with ZipFile(original) as source, ZipFile(output) as target:
        for name in source.namelist():
            if name not in (HOOK, 'fabric.mod.json'):
                assert source.read(name) == target.read(name), f'Unexpected change to {name}'
    # Verify the delivered jar, rather than only the loose compiled class.
    packaged_cp = os.pathsep.join(map(str, [tests, output, *dependencies]))
    run(['java', '-cp', packaged_cp, TEST_MAIN])
    (work / 'regression.txt').write_text(test_log)
    print(f'Repaired mod: {output}')
    print('Original mod left intact; all other classes, resources and nested mods preserved byte-for-byte.')


if __name__ == '__main__':
    main()
