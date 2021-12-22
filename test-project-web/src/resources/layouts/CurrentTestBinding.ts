import {inflateHtmlFile} from "android-xml-runtime";
import html from './current_test.html'

export interface CurrentTestBinding {
    _root: HTMLElement
    
}

export namespace CurrentTestBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as CurrentTestBinding
   }
}
