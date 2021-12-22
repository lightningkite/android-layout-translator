import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_toggle.html'

export interface TestToggleBinding {
    _root: HTMLElement
    
}

export namespace TestToggleBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestToggleBinding
   }
}
