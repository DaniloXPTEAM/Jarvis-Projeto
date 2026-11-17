export type GabiStage='age_01'|'age_02'|'age_03';
export type GabiOutfit='red_dress'|'blue_pajamas'|'study_outfit'|'purple_dress';
export interface GabiStageConfig {status:'AVAILABLE'|'PENDING_ASSET';models:Partial<Record<GabiOutfit,string>>;scale:number;walkSpeed:number;}
// Each future stage requires its own authored design, not just a scale change.
export const GABI_STAGES:Record<GabiStage,GabiStageConfig>={
 age_01:{status:'AVAILABLE',models:{red_dress:'assets/gabi/age_01/gabi_red_dress.glb'},scale:1,walkSpeed:1.35},
 age_02:{status:'PENDING_ASSET',models:{},scale:1,walkSpeed:1.35},
 age_03:{status:'PENDING_ASSET',models:{},scale:1,walkSpeed:1.35}
};
export const GABI_V1={stage:'age_01' as GabiStage,outfit:'red_dress' as GabiOutfit};
