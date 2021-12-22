import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_text.html'

export interface TestTextBinding {
    _root: HTMLElement
    
}

export namespace TestTextBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestTextBinding
   }
}
