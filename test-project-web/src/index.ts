import './resources/resources.scss'
import {TestToggleBinding} from "./resources/layouts/TestToggleBinding";

console.log("Inflating TestToggleBinding")
const x = TestToggleBinding.inflate()
console.log("Appending view ", x._root)
document.body.appendChild(x._root)
console.log("Done")
