import {inflateHtmlFile} from "android-xml-runtime";
import html from './current_test.html'

//! Declares com.example.painfullyvanilla.databinding.CurrentTestBinding
export interface CurrentTestBinding {
    root: HTMLElement
    
    
}

export namespace CurrentTestBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {}) as CurrentTestBinding
   }
}
