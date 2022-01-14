import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_system_edges.html'

//! Declares com.example.painfullyvanilla.databinding.TestSystemEdgesBinding
export interface TestSystemEdgesBinding {
    root: HTMLElement
    
    
}

export namespace TestSystemEdgesBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestSystemEdgesBinding
   }
}
