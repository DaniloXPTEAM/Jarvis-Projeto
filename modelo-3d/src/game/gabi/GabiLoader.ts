import * as THREE from 'three';
import {GLTFLoader,type GLTF} from 'three/addons/loaders/GLTFLoader.js';
export function disposeGabi(root:THREE.Object3D){const geometries=new Set<THREE.BufferGeometry>(),materials=new Set<THREE.Material>(),textures=new Set<THREE.Texture>();root.traverse(o=>{if(o instanceof THREE.Mesh){geometries.add(o.geometry);for(const m of Array.isArray(o.material)?o.material:[o.material]){materials.add(m);for(const v of Object.values(m))if(v instanceof THREE.Texture)textures.add(v)}}});textures.forEach(t=>t.dispose());materials.forEach(m=>m.dispose());geometries.forEach(g=>g.dispose());root.removeFromParent();}
export class GabiLoader {
 constructor(private loader:Pick<GLTFLoader,'loadAsync'>=new GLTFLoader()){}
 async load(url:string):Promise<GLTF>{const gltf=await this.loader.loadAsync(url);let rigged=false;gltf.scene.traverse(o=>{if(o instanceof THREE.SkinnedMesh&&o.skeleton.bones.length)rigged=true;if(o instanceof THREE.Mesh){o.castShadow=true;o.receiveShadow=true}});if(!rigged){disposeGabi(gltf.scene);throw new Error('PENDING_ASSET: expected a rigged humanoid GLB');}return gltf;}
}
