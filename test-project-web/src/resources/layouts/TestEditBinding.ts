import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_edit.html'

export interface TestEditBinding {
    _root: HTMLElement
    
}

export namespace TestEditBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestEditBinding
   }
}
