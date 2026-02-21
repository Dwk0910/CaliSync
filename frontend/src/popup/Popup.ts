export type Popup<T = object> = T & {
    close: () => void;
}