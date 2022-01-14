import './resources/resources.scss'
import {TestToggleBinding} from "./resources/layouts/TestToggleBinding";

console.log("Inflating TestToggleBinding")
const x = TestToggleBinding.inflate()
console.log("Appending view ", x.root)
document.body.appendChild(x.root)
console.log("Done")
console.log(x)
