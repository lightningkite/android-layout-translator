import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_slider.html'

//! Declares com.example.painfullyvanilla.databinding.TestSliderBinding
export interface TestSliderBinding {
    root: HTMLElement
    seek: HTMLInputElement
    bar1: HTMLInputElement
    bar2: HTMLInputElement
    bar3: HTMLInputElement
    
}

export namespace TestSliderBinding {
   export function inflate() {
       return inflateHtmlFile(html, ["seek", "bar1", "bar2", "bar3"], {}) as TestSliderBinding
   }
}
