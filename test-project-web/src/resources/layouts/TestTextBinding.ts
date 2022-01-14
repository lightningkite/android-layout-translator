import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_text.html'

//! Declares com.example.painfullyvanilla.databinding.TestTextBinding
export interface TestTextBinding {
    root: HTMLElement
    
    
}

export namespace TestTextBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestTextBinding
   }
}
