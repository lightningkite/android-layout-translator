export interface SomeBinding extends Record<string, HTMLElement | Record<string, HTMLElement> | SomeBinding> {
    root: HTMLElement
}
export function inflateHtmlFile(
    html: string,
    identifiers: Array<string>,
    compoundIdentifiers: Record<string, Array<string>>,
    includes: Record<string, ()=>SomeBinding>
): SomeBinding {
    const holder = document.createElement("div")
    holder.innerHTML = html
    const root = holder.firstElementChild!.firstElementChild! as HTMLElement
    return makeRecord(root, identifiers, compoundIdentifiers, includes)
}
function makeRecord(
    root: HTMLElement,
    identifiers: Array<string>,
    compoundIdentifiers: Record<string, Array<string>>,
    includes: Record<string, ()=>SomeBinding>
): SomeBinding {
    const result: SomeBinding = {
        root: root as HTMLElement
    }
    for (const key of identifiers) {
        result[key] = root.getElementsByClassName(`id-${key}`)[0] as HTMLElement
    }
    for (const key in compoundIdentifiers) {
        result[key] = Object.fromEntries(
            compoundIdentifiers[key].map(subkey => [subkey, root.getElementsByClassName(`id-${key}-${subkey}`)[0] as HTMLElement])
        )
    }
    for (const key in includes) {
        const oldView = root.getElementsByClassName(`id-${key}`)[0] as HTMLElement
        const sub = includes[key]()
        sub.root.style.cssText += oldView.style.cssText
        sub.root.className += " " + oldView.className
        oldView.replaceWith(sub.root)
        result[key] = sub
    }
    return result as (Record<string, HTMLElement | Record<string, HTMLElement>> & { root: HTMLElement })
}



//! Declares androidx.viewpager.widget.ViewPager
export type ViewPager = {root: HTMLElement, container: HTMLDivElement, previous: HTMLButtonElement, next: HTMLButtonElement}

//! Declares android.widget.ImageButton
export type ImageButton = {root: HTMLButtonElement, image: HTMLImageElement}

//! Declares android.widget.RadioButton
export type RadioButton = {root: HTMLElement, input: HTMLInputElement, label?: HTMLSpanElement}

//! Declares android.widget.Switch
export type Switch = {root: HTMLElement, input: HTMLInputElement, label?: HTMLSpanElement}

//! Declares android.widget.CheckBox
export type CheckBox = {root: HTMLElement, input: HTMLInputElement, label?: HTMLSpanElement}

//! Declares android.widget.ToggleButton
export type ToggleButton = {root: HTMLLabelElement, input: HTMLInputElement, button: HTMLSpanElement}