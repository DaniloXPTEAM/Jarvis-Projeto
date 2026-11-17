import {build} from 'esbuild';import {mkdir} from 'node:fs/promises';import {spawnSync} from 'node:child_process';
await mkdir('.test',{recursive:true});await build({entryPoints:['tests/gabi-v1.test.ts'],bundle:true,platform:'node',format:'esm',outfile:'.test/gabi-v1.test.mjs'});const r=spawnSync(process.execPath,['--test','.test/gabi-v1.test.mjs'],{stdio:'inherit'});process.exit(r.status??1);
