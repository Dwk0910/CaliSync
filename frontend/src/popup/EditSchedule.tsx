import * as React from 'react';
import { useState, useEffect, useRef } from 'react';

import no_color from '../assets/editscheudle_popup_asset/no_color.png';

import { type SpecialDay, type Day } from "../App";
import { type Popup } from './Popup';

import { Lunar } from 'lunar-javascript';
import { clsx } from "clsx";

import { IoAddOutline } from 'react-icons/io5';
import { RxDragHandleDots2 } from "react-icons/rx";
import { RiDeleteBin6Line } from "react-icons/ri";
import { VscHistory } from "react-icons/vsc";

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

    // 변하는 기념일 뱃지 컨테이너 height에 따라 scheduleConainter max height도 바뀌어야 아래 버튼에 영향이 안감
    const [ scheduleContainerMaxH, setScheduleContainerMaxH ] = useState<string>("max-h-95");
    const listRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        (async () => {
            const el = listRef.current;
            if (!el) return;

            // 기념일 뱃지 컨테이너가 2줄 이상으로 길어졌을 경우
            if (el.clientHeight > 20) setScheduleContainerMaxH("max-h-75")
            else if (el.clientHeight != 0) setScheduleContainerMaxH("max-h-83")
        })()
    }, [holidayInf])

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
                <div
                    ref={listRef}
                    className={clsx(
                    holidayInf.specdays.length !== 0 && "flex gap-2 flex-wrap mt-1 min-h-5 max-h-12 overflow-y-scroll"
                )}>
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
                <div className={"w-full border-b border-neutral-600 my-2"}/>
                <div className={"w-full flex justify-between"}>
                    <div className={"flex gap-2"}>
                        <div className={"inline-flex items-center justify-center p-2 rounded-[5px] bg-neutral-500 text-[1.2rem]"}><IoAddOutline /></div>
                        <div className={"inline-flex items-center justify-center p-2 rounded-[5px] bg-neutral-500"}><VscHistory /></div>
                        <div className={"inline-flex items-center justify-center p-2 rounded-[5px] bg-neutral-500"}><RiDeleteBin6Line /></div>
                    </div>
                    <div className={"flex items-center justify-center"}>
                        <div className={"border border-gray-400 rounded-[5px] overflow-hidden"}>
                            <img src={no_color} alt="n" className={"w-7 h-7 -p-1"}/>
                        </div>
                    </div>
                </div>
                <div className={clsx(
                    "mt-4 flex flex-col gap-3 overflow-y-scroll",
                    scheduleContainerMaxH
                )} style={{ scrollbarWidth: "none" }}>
                    {day.schedules.length === 0 ? (
                        <span className={"pb-2 text-gray-400 text-center mt-20"}>
                            이 날은 일정 및 이벤트가 없습니다.
                        </span>
                    ) : day.schedules.map((i, idx) => {
                        return (
                            // Container
                            <div key={`daycontent-${idx}`} className={"w-full shrink-0"}>
                                {/*// Content*/}
                                <div className={clsx(
                                    "bg-neutral-800 p-2 pl-4 overflow-x-hidden text-wrap line-clamp-2 flex items-center rounded-[5px] border border-gray-600",
                                    "flex"
                                )}>
                                    <div className={"w-[5%] flex justify-center"}>
                                        <span className={"text-[1.7rem] text-gray-400"}><RxDragHandleDots2 size={23}/></span>
                                    </div>
                                    <span className={"w-[95%] pl-4 pr-2 text-wrap wrap-break-word"}>{i.content}</span>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
            <div className={"flex my-5"}>
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