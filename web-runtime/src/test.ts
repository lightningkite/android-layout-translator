import {inflateHtmlFile} from "./index";

export interface TestBinding {
    _root: HTMLElement
    feedback: HTMLParagraphElement
    email: HTMLInputElement
    password: HTMLInputElement
    submit: HTMLButtonElement
}
export namespace TestBinding {
    export function inflate(): TestBinding {
        return inflateHtmlFile("", "feedback", "email", "password", "submit") as TestBinding
    }
}

export interface Strings {
    test: string
}
export namespace DefaultStrings {
    export const test = "asdf"
}
export const strings: Strings = Object.assign({}, DefaultStrings)