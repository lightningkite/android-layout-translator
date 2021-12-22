import {inflateHtmlFile} from "android-xml-runtime";
import html from './main_fragment.html'

export interface MainFragmentBinding {
    _root: HTMLElement
    
}

export namespace MainFragmentBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as MainFragmentBinding
   }
}
