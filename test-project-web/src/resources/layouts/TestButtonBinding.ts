import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_button.html'

//! Declares com.example.painfullyvanilla.databinding.TestButtonBinding
export interface TestButtonBinding {
    root: HTMLElement
    
    
}

export namespace TestButtonBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestButtonBinding
   }
}
