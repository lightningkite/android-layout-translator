import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_progress.html'

export interface TestProgressBinding {
    _root: HTMLElement
    
}

export namespace TestProgressBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestProgressBinding
   }
}
