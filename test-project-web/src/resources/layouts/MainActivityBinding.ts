import {inflateHtmlFile} from "android-xml-runtime";
import html from './main_activity.html'

//! Declares com.example.painfullyvanilla.databinding.MainActivityBinding
export interface MainActivityBinding {
    root: HTMLElement
    container: HTMLDivElement
    
}

export namespace MainActivityBinding {
   export function inflate() {
       return inflateHtmlFile(html, ["container"], {}) as MainActivityBinding
   }
}
