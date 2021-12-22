import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_button.html'

export interface TestButtonBinding {
    _root: HTMLElement
    
}

export namespace TestButtonBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestButtonBinding
   }
}
