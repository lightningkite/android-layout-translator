import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_attributes.html'

//! Declares com.example.painfullyvanilla.databinding.TestAttributesBinding
export interface TestAttributesBinding {
    root: HTMLElement
    
    
}

export namespace TestAttributesBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestAttributesBinding
   }
}
