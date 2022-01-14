export function inflateHtmlFile(
    html: string,
    identifiers: Array<string>,
    compoundIdentifiers: Record<string, Array<string>>
): Record<string, HTMLElement | Record<string, HTMLElement>> & { root: HTMLElement } {
    const result: Record<string, HTMLElement | Record<string, HTMLElement>> = {}
    const holder = document.createElement("div")
    holder.innerHTML = html
    console.log(holder)
    const root = holder.firstElementChild!.firstElementChild!
    result.root = root as HTMLElement
    for (const key of identifiers) {
        result[key] = root.getElementsByClassName(`id-${key}`)[0] as HTMLElement
    }
    for (const key in compoundIdentifiers) {
        result[key] = Object.fromEntries(
            compoundIdentifiers[key].map(subkey => [subkey, root.getElementsByClassName(`id-${key}-${subkey}`)[0] as HTMLElement])
        )
    }
    return result as (Record<string, HTMLElement | Record<string, HTMLElement>> & { root: HTMLElement })
}
