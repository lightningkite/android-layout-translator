import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_attributes.html'

export interface TestAttributesBinding {
    _root: HTMLElement
    
}

export namespace TestAttributesBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestAttributesBinding
   }
}
