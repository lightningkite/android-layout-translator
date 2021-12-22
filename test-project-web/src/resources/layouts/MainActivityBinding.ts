import {inflateHtmlFile} from "android-xml-runtime";
import html from './main_activity.html'

export interface MainActivityBinding {
    _root: HTMLElement
    
}

export namespace MainActivityBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as MainActivityBinding
   }
}
