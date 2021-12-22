import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_slider.html'

export interface TestSliderBinding {
    _root: HTMLElement
    
}

export namespace TestSliderBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestSliderBinding
   }
}
