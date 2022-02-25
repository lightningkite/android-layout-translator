export interface SomeBinding extends Record<string, HTMLElement | SomeBinding | null> {
    root: HTMLElement
}

export function inflateHtmlFile(
    html: string,
    identifiers: Array<string>,
    compoundIdentifiers: Record<string, Array<string>>,
    includes: Record<string, () => SomeBinding>
): SomeBinding {
    const holder = document.createElement("div")
    holder.innerHTML = html
    const root = holder.firstElementChild!.firstElementChild! as HTMLElement
    return makeRecord(root, identifiers, compoundIdentifiers, includes)
}

function getBySudoId(element: HTMLElement, key: string): HTMLElement | null {
    const c = `id-${key}`
    if (element.classList.contains(c)) return element
    return element.getElementsByClassName(c).item(0) as HTMLElement | null
}

function makeRecord(
    root: HTMLElement,
    identifiers: Array<string>,
    compoundIdentifiers: Record<string, Array<string>>,
    includes: Record<string, () => SomeBinding>
): SomeBinding {
    const result: SomeBinding = {
        root: root as HTMLElement
    }
    for (const key of identifiers) {
        result[key] = getBySudoId(root, key)
    }
    for (const key in compoundIdentifiers) {
        const raw: any = getBySudoId(root, key)
        if (raw === null) {
            result[key] = null
            continue
        }
        for (const subkey of compoundIdentifiers[key]) {
            raw[subkey] = getBySudoId(root, `${key}-${subkey}`)
        }
        result[key] = raw
    }
    for (const key in includes) {
        const oldView = getBySudoId(root, key)
        if (oldView === null) {
            result[key] = null
            continue
        }
        const sub = includes[key]()
        sub.root.style.cssText += oldView.style.cssText
        sub.root.className += " " + oldView.className
        oldView.replaceWith(sub.root)
        result[key] = sub
    }
    return result
}

//! Declares androidx.viewpager.widget.ViewPager
export type ViewPager =
    HTMLLabelElement
    & { container: HTMLDivElement, previous: HTMLButtonElement, next: HTMLButtonElement }

//! Declares android.widget.ImageButton
export type ImageButton = HTMLButtonElement & { image: HTMLImageElement }

export type BooleanControl = HTMLLabelElement & { input: HTMLInputElement, label?: HTMLSpanElement }

export function setBackground(view: HTMLElement, drawable: { name: string, file?: string }) {
    view.classList.remove(...[...view.classList].filter(x => x.startsWith("drawable-")))
    view.classList.add(`drawable-${drawable.name}`)
}
