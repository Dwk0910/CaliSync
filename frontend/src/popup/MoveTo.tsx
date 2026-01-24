import { useState, useEffect, type SetStateAction, type Dispatch } from 'react';
import { clsx } from 'clsx';

export default function MoveTo({ date, setDate, close }: { date: Date, setDate: Dispatch<SetStateAction<Date>>, close: () => void }) {
    const [disabled, setDisabled] = useState<boolean>(true);

    const [year, setYear] = useState<number>(0);
    const [month, setMonth] = useState<number>(0);

    useEffect(() => {
        setYear(date.getFullYear());
        setMonth(date.getMonth());
    }, [date])

    useEffect(() => {
        if ((Number.isNaN(Number(year)) || Number.isNaN(Number(month))) || 0 > month + 1 || month + 1 > 12 || year < 1900 ) {
            setDisabled(true);
            return;
        }

        if (year == date.getFullYear() && month == date.getMonth()) setDisabled(true);
        else setDisabled(false);
    }, [year, month, date]);

    return (
        <div className={"w-full h-full text-white flex flex-col"}>
            <span className={"text-3xl font-suite font-bold"}>이동</span>
            <div className={"flex flex-col w-full h-full justify-between"}>
                <div>
                    <span className={"font-suite text-gray-400"}>다음 달력으로 이동하기</span>
                    <div className={"mt-5"}>
                        <div className={"flex items-center"}>
                            <input type={"number"} className={"w-12 h-12 font-suite text-[1.2rem] border-b border-gray-400 outline-none"} value={!Number.isNaN(Number(year)) ? year : ''} onChange={(e) => setYear(parseInt(e.target.value))} />
                            <span className={"font-suite font-bold ml-2 text-[1.1rem]"}>년</span>
                            <input type={"number"} className={"w-10 h-12 font-suite text-[2rem] font-bold text-center ml-2 border-b border-gray-400 outline-none"} value={!Number.isNaN(Number(month)) ? month + 1 : ''} onChange={(e) => setMonth(parseInt(e.target.value) - 1)} />
                            <span className={"font-suite ml-2 text-[2rem]"}>월</span>
                        </div>
                    </div>
                </div>
                <div className={"mb-10"}>
                    <div className={clsx(
                        "flex justify-center items-center w-full h-12 rounded-lg",
                        "transition-all duration-200 ease-in-out",
                        disabled ? "bg-neutral-600" : "bg-blue-500")
                    } onClick={() => {
                        if (disabled) return;
                        setDate(new Date(year, month));
                        close();
                    }}>
                        <span className={"font-suite text-xl"}>이동</span>
                    </div>
                </div>
            </div>
        </div>
    );
}