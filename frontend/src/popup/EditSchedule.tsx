import * as React from 'react';
import { useState } from 'react';

import { type SpecialDay, type Day } from "../App";
import { type Popup } from './Popup';

import { Lunar } from 'lunar-javascript';
import { clsx } from "clsx";

type Props = Popup<{
    showingCalendar: Date,
    now: Date,
    day: Day,
    holidayInf: {
        isHoliday: boolean;
        specdays: SpecialDay[];
    };
    getDay: (date: Date, isHoliday: boolean) => React.ReactNode;
}>;

export default function EditSchedule({ showingCalendar, now, day, holidayInf, getDay, close }: Props) {
    const [ daypopup_button_active ] = useState<boolean>(false);

    const currentDay = parseInt(day.date.slice(-2));

    // Schedule Setting Popup (Day popup)
    return (
        <div className={"w-full h-full flex flex-col font-suite text-white justify-between"}>
            <div className={"flex flex-col"}>
                <span className={"mx-auto mb-3 text-white text-[1rem]"}>일정 수정</span>
                <span className={"text-[1.5rem] -mt-2 -mb-1 text-gray-300"}>{ showingCalendar.getFullYear() }년</span>
                <div>
                    <span className={"text-[2rem]"}>
                        <span>{ showingCalendar.getMonth() + 1 }월</span>
                        <span className={"ml-2"}>{ currentDay }일</span>
                    </span>
                    {(() => {
                        const date = new Date(showingCalendar.getFullYear(), showingCalendar.getMonth(), currentDay);
                        const lunar = Lunar.fromDate(date);
                        return (
                            <span className={"ml-3 text-gray-400"}>{ getDay(date, holidayInf.isHoliday) }<span className={"mx-2"}>·</span>(음) { lunar.getMonth() }월 { lunar.getDay() }일</span>
                        )
                    })()}
                </div>
                <div className={"flex gap-2 flex-wrap mt-1 min-h-5 max-h-12 overflow-y-scroll"}>
                    {now.getFullYear() == showingCalendar.getFullYear() && now.getMonth() == showingCalendar.getMonth() && now.getDate() == currentDay && (
                        <div className={"inline-block px-2 h-5 text-[0.9rem] rounded-[5px] bg-blue-900 text-white font-bold"}>
                            오늘
                        </div>
                    )}
                    {holidayInf.specdays.map((i, idx) => {
                        return (
                            <div key={`specialday-${idx}`} className={
                                clsx(
                                    "inline-block px-2 h-5 text-[0.9rem] rounded-[5px]",
                                    "truncate",
                                    i.type === "holi" && "bg-red-700 text-red-100 font-bold",
                                    i.type === "rest" && "bg-red-500 font-bold",
                                    i.type === "anni" && "bg-purple-400 text-black",
                                    i.type === "tfst" && "bg-[#F9A825]",
                                    i.type === "other" && "bg-gray-400 text-black"
                                )
                            }>{i.name}</div>
                        );
                    })}
                </div>
                <div className={"mt-2 flex flex-col gap-1 overflow-y-scroll h-80"}>
                    {day.schedules.length === 0 && (
                        <span className={"pb-2 text-gray-400 text-center mt-20"}>
                            이 날은 스케줄 및 이벤트가 없습니다.
                        </span>
                    )}
                    {day.schedules.map((i, idx) => {
                        return (
                            <>
                                <span key={idx}>{i.content}</span>
                            </>
                        );
                    })}
                </div>
            </div>
            <div className={"py-5 flex mb-5"}>
                <div className={clsx(
                    "flex justify-center items-center w-[50%] h-12 rounded-lg",
                    "transition-all duration-200 ease-in-out border border-gray-600",
                    "bg-neutral-500"
                )} onClick={() => close()}>
                    <span className={"font-suite text-xl"}>취소</span>
                </div>
                <div className={clsx(
                    "flex justify-center items-center ml-5 w-[50%] h-12 rounded-lg",
                    "transition-all duration-200 ease-in-out",
                    daypopup_button_active ? "bg-green-600/90" : "bg-neutral-600")
                } onClick={() => {
                    if (daypopup_button_active) return;
                    close();
                }}>
                    <span className={"font-suite text-xl"}>저장</span>
                </div>
            </div>
        </div>
    );
}