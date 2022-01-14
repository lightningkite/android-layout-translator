import {inflateHtmlFile} from "android-xml-runtime";
import html from './main_fragment.html'

//! Declares com.example.painfullyvanilla.databinding.MainFragmentBinding
export interface MainFragmentBinding {
    root: HTMLElement
    focusTest: HTMLInputElement
    scrollView: HTMLDivElement
    
}

export namespace MainFragmentBinding {
   export function inflate() {
       return inflateHtmlFile(html, ["focusTest", "scrollView"], {}) as MainFragmentBinding
   }
}
