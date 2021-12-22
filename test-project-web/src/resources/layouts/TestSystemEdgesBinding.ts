import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_system_edges.html'

export interface TestSystemEdgesBinding {
    _root: HTMLElement
    
}

export namespace TestSystemEdgesBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestSystemEdgesBinding
   }
}
