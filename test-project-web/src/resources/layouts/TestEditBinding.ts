import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_edit.html'

//! Declares com.example.painfullyvanilla.databinding.TestEditBinding
export interface TestEditBinding {
    root: HTMLElement
    
    
}

export namespace TestEditBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestEditBinding
   }
}
