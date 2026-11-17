import type { GabiState } from '../../types';
export const GABI_ANIMATIONS = {idle:'idle_breathe',walk:'walk',turnLeft:'turn_left',turnRight:'turn_right',listen:'listen',present:'present_talk',sitDown:'sit_down',standUp:'stand_up',read:'read',study:'study',eat:'eat',yawn:'yawn',lieDown:'lie_down',sleep:'sleep_breathe',wakeUp:'wake_up'} as const;
export type GabiAnimation = typeof GABI_ANIMATIONS[keyof typeof GABI_ANIMATIONS];
export const LOOP_CLIPS = new Set<string>(['idle_breathe','walk','sleep_breathe','read','study','eat','listen']);
export function animationForState(state:GabiState,moving:boolean,turn:number):string {
 if(state==='WALK')return moving?'walk':'idle_breathe';
 const names:Partial<Record<GabiState,string>>={IDLE:'idle_breathe',TURN:turn<0?'turn_left':'turn_right',SIT:'sit_down',STAND_UP:'stand_up',READ:'read',STUDY:'study',WRITE:'study',EAT:'eat',DRINK:'drink',PLAY:'play_toy',SLEEP:'sleep_breathe',WAKE_UP:'wake_up',TIRED:'yawn',HAPPY:'present_talk',WARDROBE:'wardrobe_browse',CHANGE_CLOTHES:'change_clothes',BATHROOM:'bath_idle_hidden',HUNGRY:'hungry_react',BORED:'bored_react'};
 return names[state]??'idle_breathe';
}
