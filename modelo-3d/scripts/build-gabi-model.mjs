import * as T from 'three';
import {mergeGeometries} from 'three/addons/utils/BufferGeometryUtils.js';
import {GLTFExporter} from 'three/addons/exporters/GLTFExporter.js';
import fs from 'node:fs';
// Exporter supports browser FileReader; this adapter only reads Blob data in Node.
globalThis.FileReader=class{readAsArrayBuffer(b){b.arrayBuffer().then(v=>{this.result=v;this.onloadend?.()})}readAsDataURL(b){b.arrayBuffer().then(v=>{this.result=`data:${b.type};base64,${Buffer.from(v).toString('base64')}`;this.onloadend?.()})}};
const root=new T.Group();root.name='Gabi_age_01_red_dress';root.userData={version:'1.0',authored:'procedural mesh with humanoid skin and authored clips',reference:'06-gabi-idle.png',visualApproval:'REQUIRES_REVIEW'};
const bones=[],index={},bind={};
function bone(name,parent,pos){const b=new T.Bone();b.name=name;b.position.set(...pos);(parent?bones[index[parent]]:root).add(b);index[name]=bones.length;bones.push(b);return b;}
bone('Hips',null,[0,.49,0]);bone('Spine','Hips',[0,.13,0]);bone('Chest','Spine',[0,.12,0]);bone('Neck','Chest',[0,.09,0]);bone('Head','Neck',[0,.13,0]);bone('EyeSurface','Head',[0,0,0]);
for(const [side,sign]of [['L',-1],['R',1]]){bone('UpperArm_'+side,'Chest',[sign*.125,.015,0]);bone('Forearm_'+side,'UpperArm_'+side,[0,-.14,0]);bone('Hand_'+side,'Forearm_'+side,[0,-.125,0]);bone('Hand_'+side+'_Attach','Hand_'+side,[0,-.025,.018]);bone('Thigh_'+side,'Hips',[sign*.072,-.01,0]);bone('Shin_'+side,'Thigh_'+side,[0,-.19,0]);bone('Foot_'+side,'Shin_'+side,[0,-.20,.016]);bone('Lid_'+side,'Head',[sign*.079,.018,.133]);}
root.updateMatrixWorld(true);bones.forEach(b=>bind[b.name]=b.getWorldPosition(new T.Vector3()));const skeleton=new T.Skeleton(bones);skeleton.calculateInverses();
const material=(name,color,roughness=.7)=>{const m=new T.MeshStandardMaterial({name,color,roughness});return m};
const skin=material('Skin_peach','#f4bd95'),cheek=material('Soft_blush','#eaa789'),freckle=material('Freckles','#b97c58'),red=material('Red_dress','#bd142e'),hem=material('Dress_seam','#99152b'),hair=material('Copper_hair','#ba4815',.48),hairLight=material('Copper_highlights','#da691f',.48),hairDark=material('Copper_shadow','#94340f'),black=material('Black_boots','#292426',.38),sole=material('Boot_soles','#1a1719'),white=material('Eye_white','#fff9ef',.22),iris=material('Dark_brown_iris','#653114',.24),pupil=material('Pupil','#100b08',.1),mouth=material('Smile','#8e392e');
function skinMesh(g,m,boneName,blend){const count=g.attributes.position.count,ids=[],weights=[];for(let i=0;i<count;i++){const y=g.attributes.position.getY(i);const w=blend?T.MathUtils.clamp((y-blend.low)/(blend.high-blend.low),0,1):0;ids.push(index[boneName],blend?index[blend.bone]:0,0,0);weights.push(1-w,w,0,0)}g.setAttribute('skinIndex',new T.Uint16BufferAttribute(ids,4));g.setAttribute('skinWeight',new T.Float32BufferAttribute(weights,4));const mesh=new T.SkinnedMesh(g,m);mesh.name=m.name+'_'+root.children.length;mesh.bind(skeleton,new T.Matrix4());mesh.castShadow=true;mesh.receiveShadow=true;root.add(mesh);return mesh;}
function ellipsoid(x,y,z,sx,sy,sz,m,b='Head',segments=32){const g=new T.SphereGeometry(1,segments,20);g.scale(sx,sy,sz);g.translate(x,y,z);return skinMesh(g,m,b)}
function tube(points,r,m,b='Head',blend){const curve=new T.CatmullRomCurve3(points.map(p=>new T.Vector3(...p)));return skinMesh(new T.TubeGeometry(curve,Math.max(10,points.length*5),r,7,false),m,b,blend)}
function limb(a,b,r1,r2,m,boneName){const delta=new T.Vector3(...b).sub(new T.Vector3(...a));const g=new T.CylinderGeometry(r2,r1,delta.length(),20,5);g.applyQuaternion(new T.Quaternion().setFromUnitVectors(new T.Vector3(0,1,0),delta.clone().normalize()));g.translate(...new T.Vector3(...a).add(new T.Vector3(...b)).multiplyScalar(.5).toArray());return skinMesh(g,m,boneName);}
// Soft child proportions, shaped cheeks and tapered chin rather than a spherical head.
const face=new T.SphereGeometry(1,48,32);const v=face.attributes.position;
for(let i=0;i<v.count;i++){let x=v.getX(i),y=v.getY(i),z=v.getZ(i);const width=y<-.15?1-.19*(-y-.15):1+.025*Math.exp(-Math.pow((y+.15)*4,2));x*=.175*width;z*=.143; if(z>0)z+=.013*Math.exp(-Math.pow(y*2,2));v.setXYZ(i,x,.979+y*.187,z+.006)}face.computeVertexNormals();skinMesh(face,skin,'Head');
ellipsoid(0,.813,0,.045,.065,.04,skin,'Neck');ellipsoid(0,.68,0,.112,.125,.067,red,'Spine');
// Fitted sleeveless bodice, flared pleated skirt with continuous side surface.
let skirt=new T.CylinderGeometry(.102,.208,.275,80,12,true);let sp=skirt.attributes.position;for(let i=0;i<sp.count;i++){let x=sp.getX(i),y=sp.getY(i),z=sp.getZ(i);const t=(.1375-y)/.275,angle=Math.atan2(z,x),fold=1+.035*Math.cos(angle*18)*t;sp.setXYZ(i,x*fold,y+.46,z*.66*fold)}skirt.computeVertexNormals();skinMesh(skirt,red,'Hips');
const waist=new T.TorusGeometry(.103,.004,6,64);waist.rotateX(Math.PI/2);waist.scale(1,1,.68);waist.translate(0,.598,0);skinMesh(waist,hem,'Spine');
const neckline=new T.TorusGeometry(.045,.0035,6,40);neckline.rotateX(Math.PI/2);neckline.scale(1,1,.82);neckline.translate(0,.789,.012);skinMesh(neckline,hem,'Chest');
for(const [side,sign]of [['L',-1],['R',1]]){const x=sign*.126;
ellipsoid(x,.736,0,.042,.051,.039,skin,'UpperArm_'+side);limb([x,.736,0],[x,.602,0],.029,.034,skin,'UpperArm_'+side);ellipsoid(x,.602,0,.029,.03,.027,skin,'Forearm_'+side);limb([x,.602,0],[x,.477,.005],.023,.028,skin,'Forearm_'+side);ellipsoid(x,.463,.007,.029,.04,.018,skin,'Hand_'+side);
for(let f=0;f<4;f++){const fx=x+(f-1.5)*.012;limb([fx,.448,.012],[fx,.420+(f===0||f===3?.008:0),.015],.005,.006,skin,'Hand_'+side)}ellipsoid(x-sign*.024,.452,.018,.01,.02,.009,skin,'Hand_'+side);
limb([sign*.072,.478,0],[sign*.072,.29,0],.032,.041,skin,'Thigh_'+side);ellipsoid(sign*.072,.29,0,.032,.033,.032,skin,'Shin_'+side);limb([sign*.072,.29,0],[sign*.072,.09,.013],.024,.032,skin,'Shin_'+side);
ellipsoid(sign*.072,.063,.031,.041,.056,.068,black,'Foot_'+side);limb([sign*.072,.066,.016],[sign*.072,.155,.016],.037,.034,black,'Shin_'+side);ellipsoid(sign*.072,.021,.040,.043,.012,.071,sole,'Foot_'+side);
ellipsoid(sign*.172,.962,.008,.027,.04,.020,skin);ellipsoid(sign*.182,.962,.021,.013,.021,.008,cheek);
// Large oval eyes embedded in the face with brown iris, pupil and tiny highlights.
ellipsoid(sign*.079,.989,.139,.061,.068,.025,white,'EyeSurface');ellipsoid(sign*.079,.988,.162,.034,.043,.009,iris,'EyeSurface');ellipsoid(sign*.079,.990,.171,.022,.029,.006,pupil,'EyeSurface');ellipsoid(sign*.067,1.008,.178,.009,.011,.003,white,'EyeSurface');ellipsoid(sign*.090,.977,.178,.0035,.004,.002,white,'EyeSurface');
const lid=ellipsoid(sign*.079,.990,.168,.063,.071,.025,skin,'Lid_'+side);bones[index['Lid_'+side]].position.z=-.12;
const line=[];for(let j=0;j<=12;j++){const theta=j/12*Math.PI;line.push([sign*.079+Math.cos(theta)*.061,.990+Math.sin(theta)*.068,.149])}tube(line,.003,black);
for(let j=0;j<3;j++){let xx=sign*(.122+j*.006),yy=1.025+j*.006;tube([[xx,yy,.147],[xx+sign*.012,yy+.013,.145]],.002,black)}
tube([[sign*.034,1.068,.129],[sign*.078,1.086,.119],[sign*.124,1.071,.101]],.008,hairDark);
ellipsoid(sign*.116,.935,.122,.029,.014,.004,cheek);
for(let j=0;j<6;j++){const fx=sign*(.060+(j%3)*.018),fy=.945-Math.floor(j/3)*.014;tube([[fx,fy,.146-Math.abs(fx)*.10],[fx+.001,fy+.001,.146-Math.abs(fx)*.10]],.0019,freckle);}
}
ellipsoid(0,.945,.161,.026,.020,.025,skin);ellipsoid(0,.936,.184,.014,.005,.004,cheek);
tube([[-.056,.912,.131],[-.025,.899,.148],[0,.898,.151],[.028,.902,.145],[.054,.915,.128]],.0045,mouth);tube([[-.030,.895,.148],[0,.890,.150],[.027,.898,.143]],.003,cheek);
// Continuous crown and long sculpted wavy locks; no texture billboard.
const cap=new T.SphereGeometry(1,48,24,0,Math.PI*2,0,Math.PI*.38);cap.scale(.184,.203,.156);cap.translate(0,.989,-.003);skinMesh(cap,hair,'Head');
for(let k=0;k<19;k++){const a=Math.PI*.04+k/18*Math.PI*.92;const x=Math.cos(a)*.158,z=-Math.sin(a)*.13;const pts=[];for(let j=0;j<9;j++){const t=j/8;pts.push([x+Math.sin(t*13+k*.7)*.019,.999-t*(.51+(k%3)*.017),z+Math.cos(t*12+k)*.020])}tube(pts,.035,k%3===0?hairLight:hair,'Head');for(let strand=0;strand<2;strand++)tube(pts.map(([x,y,z])=>[x+.016*(strand?1:-1),y,z+.025]),.0035,k%2?hairLight:hairDark,'Head');}
for(const sign of [-1,1]){for(let k=0;k<5;k++){const pts=[[sign*.010,1.166-k*.004,.003],[sign*.060,1.158,.087],[sign*.133,1.105-k*.007,.139],[sign*.17,1.016-k*.012,.094],[sign*(.176+k*.006),.897,.065],[sign*(.191+k*.006),.773,.076],[sign*.180,.646+k*.008,.048],[sign*.201,.529+k*.010,.019]];tube(pts,.023,k%2?hair:hairLight);tube(pts.map(([x,y,z])=>[x,y,z+.020]),.0025,hairDark);}}
root.updateMatrixWorld(true);
// Skeleton-space animation tracks. Global translations stay controlled by the game.
const clips=[];function quatTrack(name,times,poses){const values=[];for(const [x,y,z]of poses)values.push(...new T.Quaternion().setFromEuler(new T.Euler(x,y,z)).toArray());return new T.QuaternionKeyframeTrack(name+'.quaternion',times,values)}
function clip(name,duration,fn){const tracks=fn(duration);clips.push(new T.AnimationClip(name,duration,tracks));}
const osc=(name,d,axis,amp,cycles=1,bias=0)=>{const times=Array.from({length:33},(_,i)=>d*i/32);return quatTrack(name,times,times.map(t=>{const p=[0,0,0];p[axis]=bias+Math.sin(t/d*Math.PI*2*cycles)*amp;return p}))};
clip('idle_breathe',4,d=>[new T.VectorKeyframeTrack('EyeSurface.scale',[0,2.6,2.68,2.8,2.9,4],[1,1,.001,.001,1,1].flatMap(v=>[v,v,v])),osc('Chest',d,0,.012),osc('Head',d,1,.035),...['L','R'].map(side=>new T.VectorKeyframeTrack('Lid_'+side+'.position',[0,2.6,2.68,2.8,2.9,4],[-1,-1,1,1,-1,-1].flatMap(v=>[side==='L'?-.079:.079,.018,v===1?.133:-.12])))]);
clip('walk',.9,d=>{let ts=Array.from({length:33},(_,i)=>d*i/32),tracks=[];for(const [side,sign]of [['L',1],['R',-1]]){tracks.push(osc('Thigh_'+side,d,0,.45*sign),osc('UpperArm_'+side,d,0,-.27*sign));tracks.push(quatTrack('Shin_'+side,ts,ts.map(t=>[Math.max(0,-Math.sin(t/d*Math.PI*2)*sign)*.65,0,0])))}tracks.push(osc('Spine',d,1,.035));return tracks});
for(const [name,sign]of [['turn_left',-1],['turn_right',1]])clip(name,.55,d=>[quatTrack('Head',[0,d*.35,d],[[0,0,0],[0,sign*.18,0],[0,0,0]]),quatTrack('Thigh_L',[0,d*.5,d],[[0,0,0],[-.13,0,0],[0,0,0]]),quatTrack('Thigh_R',[0,d*.5,d],[[0,0,0],[.13,0,0],[0,0,0]])]);
clip('listen',3,d=>[osc('Head',d,2,.045),quatTrack('UpperArm_R',[0,.6,2.4,d],[[0,0,0],[-.5,0,-.8],[-.5,0,-.8],[0,0,0]]),quatTrack('Forearm_R',[0,.6,2.4,d],[[0,0,0],[-2,0,0],[-2,0,0],[0,0,0]])]);
clip('present_talk',2.5,d=>[quatTrack('UpperArm_R',[0,.5,1.8,d],[[0,0,0],[-.6,0,-.65],[-.8,0,-.65],[0,0,0]]),osc('Head',d,1,.07)]);
const seated=(d,reverse=false)=>{const a=reverse?[-1.3,0,0]:[0,0,0],b=reverse?[0,0,0]:[-1.3,0,0];return ['L','R'].flatMap(side=>[quatTrack('Thigh_'+side,[0,d],[a,b]),quatTrack('Shin_'+side,[0,d],[reverse?[1.4,0,0]:[0,0,0],reverse?[0,0,0]:[1.4,0,0]])])};
clip('sit_down',.9,d=>seated(d));clip('stand_up',.9,d=>seated(d,true));
for(const name of ['read','study','eat'])clip(name,3,d=>[...['L','R'].flatMap(side=>[quatTrack('Thigh_'+side,[0,d],[[-1.3,0,0],[-1.3,0,0]]),quatTrack('Shin_'+side,[0,d],[[1.4,0,0],[1.4,0,0]])]),osc('Head',d,0,.025,1,.17),osc('UpperArm_L',d,0,.03,1,-.8),osc('Forearm_L',d,0,.03,1,-.65),osc('UpperArm_R',d,0,name==='eat'?.25:.035,1,-.9),osc('Forearm_R',d,0,name==='eat'?.45:.06,1,-.8)]);
clip('yawn',2.4,d=>[quatTrack('UpperArm_R',[0,.6,1.8,d],[[0,0,0],[-1.1,0,-.1],[-1.1,0,-.1],[0,0,0]]),quatTrack('Forearm_R',[0,.6,1.8,d],[[0,0,0],[-1.35,0,0],[-1.35,0,0],[0,0,0]]),osc('Head',d,0,-.07)]);
clip('lie_down',1.6,d=>[quatTrack('Hips',[0,d],[[0,0,0],[-Math.PI/2,0,0]])]);
clip('sleep_breathe',4,d=>[new T.VectorKeyframeTrack('EyeSurface.scale',[0,d],[.001,.001,.001,.001,.001,.001]),quatTrack('Hips',[0,d],[[-Math.PI/2,0,0],[-Math.PI/2,0,0]]),osc('Chest',d,0,.010),...['L','R'].map(side=>new T.VectorKeyframeTrack('Lid_'+side+'.position',[0,d],[side==='L'?-.079:.079,.018,.133,side==='L'?-.079:.079,.018,.133]))]);
clip('wake_up',2.5,d=>[quatTrack('Hips',[0,d*.75,d],[[-Math.PI/2,0,0],[0,0,0],[0,0,0]]),quatTrack('UpperArm_L',[0,d*.6,d],[[0,0,0],[-2.1,0,0],[0,0,0]]),quatTrack('UpperArm_R',[0,d*.6,d],[[0,0,0],[-2.1,0,0],[0,0,0]])]);
root.updateMatrixWorld(true);
// Merge by material to reduce mobile draw calls while preserving all skin weights.
const grouped=new Map();for(const m of [...root.children])if(m.isSkinnedMesh){if(!grouped.has(m.material))grouped.set(m.material,[]);grouped.get(m.material).push(m);}
for(const [material,meshes]of grouped){const geometry=mergeGeometries(meshes.map(m=>m.geometry),false);const mesh=new T.SkinnedMesh(geometry,material);mesh.name=material.name;mesh.bind(skeleton,new T.Matrix4());mesh.castShadow=true;mesh.receiveShadow=true;for(const m of meshes){root.remove(m);m.geometry.dispose()}root.add(mesh);}
const out=await new GLTFExporter().parseAsync(root,{binary:true,animations:clips,onlyVisible:false});
fs.mkdirSync('public/assets/gabi/age_01',{recursive:true});fs.writeFileSync('public/assets/gabi/age_01/gabi_red_dress.glb',Buffer.from(out));fs.writeFileSync('public/assets/gabi/age_01/model-report.json',JSON.stringify({bytes:out.byteLength,bones:bones.map(b=>b.name),clips:clips.map(c=>({name:c.name,duration:c.duration})),status:'FIRST_AUTHORED_VERSION_REQUIRES_VISUAL_REVIEW'},null,2));console.log('GLB',out.byteLength,'bytes',bones.length,'bones',clips.length,'clips');

fs.writeFileSync('src/game/gabi/age_01/modelUrl.ts','// Generated with the GLB; allows the built HTML to run without a separate asset fetch.\nexport const GABI_MODEL_URL = '+JSON.stringify('data:model/gltf-binary;base64,'+Buffer.from(out).toString('base64'))+';\n');
