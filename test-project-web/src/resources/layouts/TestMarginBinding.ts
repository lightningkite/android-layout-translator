import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_margin.html'

//! Declares com.example.painfullyvanilla.databinding.TestMarginBinding
export interface TestMarginBinding {
    root: HTMLElement
    scrollToTop: HTMLButtonElement
    focusTest: HTMLInputElement
    scrollView: HTMLDivElement
    
}

export namespace TestMarginBinding {
   export function inflate() {
       return inflateHtmlFile(html, ["scrollToTop", "focusTest", "scrollView"], {}) as TestMarginBinding
   }
}
