import {inflateHtmlFile} from "android-xml-runtime";
import html from './test_toggle.html'

//! Declares com.example.painfullyvanilla.databinding.TestToggleBinding
export interface TestToggleBinding {
    root: HTMLElement
    
    switchView: {root: HTMLLabelElement, input: HTMLLabelElement, label: HTMLLabelElement}
    checkView: {root: HTMLLabelElement, input: HTMLLabelElement, label: HTMLLabelElement}
    radioView: {root: HTMLLabelElement, input: HTMLLabelElement, label: HTMLLabelElement}
    switchView2: {root: HTMLLabelElement, input: HTMLLabelElement, label: HTMLLabelElement}
    checkView2: {root: HTMLLabelElement, input: HTMLLabelElement, label: HTMLLabelElement}
    radioView2: {root: HTMLLabelElement, input: HTMLLabelElement, label: HTMLLabelElement}
}

export namespace TestToggleBinding {
   export function inflate() {
       return inflateHtmlFile(html, [], {switchView: ["root", "input", "label"], checkView: ["root", "input", "label"], radioView: ["root", "input", "label"], switchView2: ["root", "input", "label"], checkView2: ["root", "input", "label"], radioView2: ["root", "input", "label"]}) as TestToggleBinding
   }
}
