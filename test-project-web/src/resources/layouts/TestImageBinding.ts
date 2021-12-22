import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_image.html'

export interface TestImageBinding {
    _root: HTMLElement
    
}

export namespace TestImageBinding {
   export function inflate() {
       return inflateHtmlFile(html, ) as TestImageBinding
   }
}
