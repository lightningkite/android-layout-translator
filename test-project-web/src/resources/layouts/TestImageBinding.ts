import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_image.html'

//! Declares com.example.painfullyvanilla.databinding.TestImageBinding
export interface TestImageBinding {
    root: HTMLElement
    
    
}

export namespace TestImageBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as TestImageBinding
   }
}
