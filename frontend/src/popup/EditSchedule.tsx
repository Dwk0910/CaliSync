import * as React from 'react';
import { useState, useEffect, useRef } from 'react';

import no_color from '../assets/editscheudle_popup_asset/no_color.png';

import { type SpecialDay, type Schedule, type Day } from "../App";
import { type Popup } from './Popup';

import { Reorder, AnimatePresence, useDragControls, motion, useAnimation } from 'framer-motion';

import { Lunar } from 'lunar-javascript';
import { clsx } from "clsx";

import { IoAddOutline } from 'react-icons/io5';
import { RiDeleteBin6Line } from "react-icons/ri";
import { RxDragHandleDots2 } from "react-icons/rx";
import { VscHistory } from "react-icons/vsc";

type Props = Popup<{
    showingCalendar: Date,
    now: Date,
    day: Day,
    holidayInf: {
        isHoliday: boolean;
        specdays: SpecialDay[];
    };
    getDayName: (date: Date, isHoliday: boolean) => React.ReactNode;
}>;

export default function EditSchedule({ showingCalendar, now, day, holidayInf, getDayName, close }: Props) {
    const [ daypopup_button_active ] = useState(false);
    const [ schedules, setSchedules ] = useState<Schedule[]>(day.schedules);

    const listRef = useRef<HTMLDivElement>(null);
    const [ scheduleContainerMaxH, setScheduleContainerMaxH ] = useState("h-90");

    useEffect(() => {
        const el = listRef.current;
        if (!el) return;

        if (el.clientHeight > 20) setScheduleContainerMaxH("h-75");
        else if (el.clientHeight !== 0) setScheduleContainerMaxH("h-83");
    }, [holidayInf]);

    useEffect(() => {
        setSchedules(day.schedules);
    }, [day]);

    const currentDay = parseInt(day.date.slice(-2));

    return (
        <div className="w-full h-full flex flex-col font-suite text-white justify-between">
            <div>
                <span className="mx-auto mb-3 block text-center">일정 수정</span>
                <span className="text-[1.5rem] -mt-2 -mb-1 text-gray-300">
                    {showingCalendar.getFullYear()}년
                </span>

                <div>
                    <span className="text-[2rem]">
                        {showingCalendar.getMonth() + 1}월
                        <span className="ml-2">{currentDay}일</span>
                    </span>
                    {(() => {
                        const date = new Date(
                            showingCalendar.getFullYear(),
                            showingCalendar.getMonth(),
                            currentDay
                        );
                        const lunar = Lunar.fromDate(date);
                        return (
                            <span className="ml-3 text-gray-400">
                                {getDayName(date, holidayInf.isHoliday)}
                                <span className="mx-2">·</span>
                                (음) {lunar.getMonth()}월 {lunar.getDay()}일
                            </span>
                        );
                    })()}
                </div>

                <div
                    ref={listRef}
                    className={clsx(
                        holidayInf.specdays.length !== 0 &&
                        "flex flex-wrap mt-1 min-h-5 max-h-12 overflow-y-scroll"
                    )}
                >
                    {now.getFullYear() === showingCalendar.getFullYear() &&
                        now.getMonth() === showingCalendar.getMonth() &&
                        now.getDate() === currentDay && (
                            <div className="px-2 h-5 text-[0.9rem] rounded bg-blue-900 font-bold">
                                오늘
                            </div>
                        )}

                    {holidayInf.specdays.map((i, idx) => (
                        <div
                            key={idx}
                            className={clsx(
                                "px-2 h-5 text-[0.9rem] rounded truncate",
                                i.type === "holi" && "bg-red-700 text-red-100 font-bold",
                                i.type === "rest" && "bg-red-500 font-bold",
                                i.type === "anni" && "bg-purple-400 text-black",
                                i.type === "tfst" && "bg-[#F9A825]",
                                i.type === "other" && "bg-gray-400 text-black"
                            )}
                        >
                            {i.name}
                        </div>
                    ))}
                </div>

                <div className="w-full border-b border-neutral-600 my-2" />

                <div className="flex justify-between">
                    <div className="flex gap-2">
                        <div className="p-2 rounded bg-neutral-500"><IoAddOutline /></div>
                        <div className="p-2 rounded bg-neutral-500"><VscHistory /></div>
                        <div className="p-2 rounded bg-neutral-500"><RiDeleteBin6Line /></div>
                    </div>
                    <div className="border border-gray-400 rounded overflow-hidden">
                        <img src={no_color} alt="nc" className="w-7 h-7" />
                    </div>
                </div>

                <div className="w-full">
                    <Reorder.Group
                        layout
                        axis="y"
                        values={schedules}
                        onReorder={setSchedules}
                        className={clsx("mt-4 overflow-y-scroll", scheduleContainerMaxH)}
                        style={{ scrollbarWidth: "none" }}
                    >
                        <AnimatePresence mode="popLayout">
                            {schedules.length === 0 ? (
                                <div className="mt-20 text-center text-gray-400">
                                    이 날은 일정 및 이벤트가 없습니다.
                                </div>
                            ) : schedules.map(item => (
                                    <ScheduleItem
                                        key={item.id}
                                        item={item}
                                        setSchedules={setSchedules}
                                    />
                                ))
                            }
                        </AnimatePresence>
                    </Reorder.Group>
                </div>
            </div>

            <div className="flex my-5">
                <div
                    className="w-1/2 h-12 flex items-center justify-center rounded-lg bg-neutral-500 border border-gray-600"
                    onClick={close}
                >
                    취소
                </div>
                <div
                    className={clsx(
                        "w-1/2 h-12 ml-5 flex items-center justify-center rounded-lg",
                        daypopup_button_active ? "bg-green-600/90" : "bg-neutral-600"
                    )}
                    onClick={() => {
                        if (!daypopup_button_active) close();
                    }}
                >
                    저장
                </div>
            </div>
        </div>
    );
}

const ScheduleItem = ({ item, setSchedules }: { item: Schedule; setSchedules: React.Dispatch<React.SetStateAction<Schedule[]>>; }) => {
    const reorderItemDragControl = useDragControls();
    const deleteItemDragControl = useDragControls();
    const deleteItemAnimation = useAnimation();

    return (
        <Reorder.Item
            value={item}
            layout
            initial={{ opacity: 1 }}
            exit={{
                opacity: 0,
                height: 0,
                marginBottom: 0,
                paddingTop: 0,
                paddingBottom: 0
            }}
            dragListener={ false }
            dragControls={ reorderItemDragControl }
            transition={{ duration: 0.2 }}
            className="relative mb-2 list-none touch-none overflow-hidden shrink-0"
        >
            <div className={"absolute flex justify-end items-center pr-2 z-0 inset-0 bg-red-400 rounded my-0.5 left-10"}>
                <RiDeleteBin6Line />
            </div>
            <motion.div
                drag={"x"}
                dragConstraints={{ left: -70, right: 0 }}
                dragElastic={{ left: 0.5, right: 0 }}
                onDragEnd={async (_, info) => {
                    if (info.offset.x <= -100) {
                        await deleteItemAnimation.start({
                            x: -400,
                            transition: { duration: 0.2 }
                        });
                        setSchedules(prev => prev.filter(p => p.id !== item.id));
                    } else if (info.offset.x < -15) {
                        await deleteItemAnimation.start({
                            x: -30,
                            transition: { duration: 0.2 }
                        });
                    } else {
                        await deleteItemAnimation.start({
                            x: 0,
                            transition: { duration: 0.2 }
                        });
                    }
                }}
                animate={ deleteItemAnimation }
                dragControls={ deleteItemDragControl }
                className={"relative flex z-10 w-full items-center bg-neutral-800 border border-gray-600 rounded"}
            >
                <div className={"text-[1.1rem] text-gray-400 mx-2"} onPointerDown={(e) => {
                    e.stopPropagation();
                    reorderItemDragControl.start(e);
                }}>
                    <RxDragHandleDots2/>
                </div>
                <div className={"w-full py-2 pr-2 touch-none"} onPointerDown={(e) => deleteItemDragControl.start(e)}>
                    {item.content}
                </div>
            </motion.div>
        </Reorder.Item>
    );
};
