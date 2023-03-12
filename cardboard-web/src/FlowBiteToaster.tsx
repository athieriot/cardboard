import {resolveValue, ToastBar, Toaster} from "react-hot-toast";
import {Toast} from "flowbite-react";
import {CheckIcon, XMarkIcon} from "@heroicons/react/24/solid";

const FlowBiteToaster = () => (
    <Toaster>
        {(t) => {
            const color = t.type === "success" ? "green" : "red"
            return <Toast>
                <div className={`inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-${color}-100 text-${color}-500 dark:bg-${color}-800 dark:text-${color}-200`}>
                    {t.type === "success" ? <CheckIcon className="h-5 w-5"/> : <XMarkIcon className="h-5 w-5"/> }
                </div>
                <div className="ml-3 text-sm font-normal">
                    {resolveValue(t.message, t)}
                </div>
                <Toast.Toggle />
            </Toast>
        }}
    </Toaster>
);

export default FlowBiteToaster