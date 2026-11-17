import * as THREE from 'three';
import {ProceduralDriver,type AnimDriver} from '../animationLayer';
import type {GabiRig} from '../gabiProvisional';
import type {GabiState} from '../types';
import {GabiLoader,disposeGabi} from './GabiLoader';
import {GABI_ANIMATIONS,LOOP_CLIPS,animationForState} from './config/gabiAnimations';
import {GABI_STAGES,GABI_V1,type GabiStage,type GabiOutfit} from './config/gabiStages';
export class GabiController implements AnimDriver {
 readonly status={model:'PENDING_ASSET',clips:{} as Record<string,'READY'|'PENDING_ASSET'>,stage:GABI_V1.stage,outfit:GABI_V1.outfit};
 private fallback:ProceduralDriver;private provisional:THREE.Object3D[];private model?:THREE.Group;private mixer?:THREE.AnimationMixer;private actions=new Map<string,THREE.AnimationAction>();private current?:THREE.AnimationAction;private requested='';private warned=new Set<string>();private disposed=false;private revision=0;private lastState?:GabiState;private turn=0;private speed=0;private lastPosition?:THREE.Vector3;private loadedVisible=false;private elapsed=0;private sleepEntry=false;private reaction?:{name:string;remaining:number};
 constructor(private rig:GabiRig,private loader=new GabiLoader()){this.fallback=new ProceduralDriver(rig);this.provisional=[...rig.group.children];Object.values(GABI_ANIMATIONS).forEach(n=>this.status.clips[n]='PENDING_ASSET');}
 async load(stage:GabiStage='age_01',outfit:GabiOutfit='red_dress',base='./',modelUrl?:string){const revision=++this.revision,config=GABI_STAGES[stage],url=config.models[outfit];if(!url){this.status.model='PENDING_ASSET';return false;}this.status.model='LOADING';try{const gltf=await this.loader.load(modelUrl??base+url);if(this.disposed||revision!==this.revision){disposeGabi(gltf.scene);return false;}this.clearModel();this.model=gltf.scene;this.model.scale.setScalar(config.scale);this.model.visible=false;this.rig.group.add(this.model);this.mixer=new THREE.AnimationMixer(this.model);this.addClips(gltf.animations);this.status.stage=stage;this.status.outfit=outfit;this.status.model='READY';return true;}catch{if(!this.disposed&&revision===this.revision)this.status.model='PENDING_ASSET';return false;}}
 addClips(clips:THREE.AnimationClip[]){if(!this.mixer)return;for(const clip of clips){if(clip.duration<=0)continue;const action=this.mixer.clipAction(clip);action.setLoop(LOOP_CLIPS.has(clip.name)?THREE.LoopRepeat:THREE.LoopOnce,LOOP_CLIPS.has(clip.name)?Infinity:1);action.clampWhenFinished=true;this.actions.set(clip.name,action);this.status.clips[clip.name]='READY';}}
 playNamed(name:string,seconds=2.5){this.reaction={name,remaining:Math.max(.1,seconds)};this.requested="";}
 setTurn(delta:number){if(Math.abs(delta)>.001)this.turn=delta;}
 setLying(v:boolean){this.fallback.setLying(v)}setSitting(v:boolean){this.fallback.setSitting(v)}
 reset(){this.fallback.reset();this.current?.stop();this.current=undefined;this.requested='';this.lastState=undefined;this.lastPosition=undefined;}
 play(name:string){if(this.requested===name)return;this.requested=name;let action=this.actions.get(name);if(!action){this.status.clips[name]='PENDING_ASSET';if(!this.warned.has(name)){this.warned.add(name);console.warn(`[GabiAnimation] PENDING_ASSET: ${name}`)}action=this.actions.get('idle_breathe');}if(action===this.current)return;this.current?.fadeOut(.2);this.current=action;if(action)action.reset().fadeIn(.2).play();}
 update(dt:number,time:number,state:GabiState,moving:boolean,reduced:boolean){if(this.disposed)return;const pos=this.rig.group.position.clone();this.speed=this.lastPosition&&dt>0?Math.hypot(pos.x-this.lastPosition.x,pos.z-this.lastPosition.z)/dt:0;this.lastPosition=pos;
 if(state!==this.lastState){this.sleepEntry=state==='SLEEP'&&this.lastState!==undefined;this.elapsed=0;this.requested='';this.lastState=state;}this.elapsed+=dt;const lie=this.actions.get('lie_down');let name=this.sleepEntry&&state==='SLEEP'&&lie&&this.elapsed<lie.getClip().duration?'lie_down':animationForState(state,moving,this.turn);if(this.reaction&&['IDLE','HAPPY'].includes(state)){name=this.reaction.name;this.reaction.remaining-=dt;if(this.reaction.remaining<=0)this.reaction=undefined;}else this.reaction=undefined;this.play(name);
 const useModel=!!this.model&&!!this.current;this.loadedVisible=useModel;this.provisional.forEach(o=>o.visible=!useModel);if(this.model)this.model.visible=useModel;
 if(useModel){this.rig.group.position.y+=( (state==='SLEEP'?.18:0)-this.rig.group.position.y)*Math.min(1,dt*3);if(this.current&&name==='walk'&&this.actions.get('walk')===this.current)this.current.timeScale=this.speed/GABI_STAGES[this.status.stage].walkSpeed;this.mixer!.update(Math.max(0,dt));}else this.fallback.update(dt,time,state,moving,reduced);
 }
 get handR(){return this.loadedVisible?this.model?.getObjectByName('Hand_R_Attach'):this.rig.anchors.handR;}
 private clearModel(){this.mixer?.stopAllAction();if(this.model){this.mixer?.uncacheRoot(this.model);disposeGabi(this.model);}this.actions.clear();this.current=undefined;this.requested='';this.model=undefined;this.mixer=undefined;Object.keys(this.status.clips).forEach(n=>this.status.clips[n]='PENDING_ASSET');}
 dispose(){this.disposed=true;++this.revision;this.clearModel();}
}
