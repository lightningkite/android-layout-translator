import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_progress.html'

//! Declares com.example.painfullyvanilla.databinding.TestProgressBinding
export interface TestProgressBinding {
    root: HTMLElement
    
    
}

export namespace TestProgressBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestProgressBinding
   }
}
