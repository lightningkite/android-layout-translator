
export function inflateHtmlFile(html: string, ...identifiers: Array<string>): Record<string, HTMLElement> & { _root: HTMLElement } {
    const result: Record<string, HTMLElement> = {}
    const holder = document.createElement("div")
    holder.innerHTML = html
    const root = holder.firstElementChild!
    result._root = root as HTMLElement
    for(const key of identifiers) {
        result[key] = root.getElementsByClassName(`id-${key}`)[0] as HTMLElement
    }
    return result as (Record<string, HTMLElement> & { _root: HTMLElement })
}