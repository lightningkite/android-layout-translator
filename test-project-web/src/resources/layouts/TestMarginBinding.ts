import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_margin.html'

export interface TestMarginBinding {
    _root: HTMLElement
    
}

export namespace TestMarginBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestMarginBinding
   }
}
