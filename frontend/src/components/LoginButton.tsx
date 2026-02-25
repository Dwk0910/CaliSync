import axios from "axios";
import { useEffect, type Dispatch, type SetStateAction } from 'react';
import { useGoogleLogin } from '@react-oauth/google';

// useGoogleLogin 훅은 App 안에서 정의할 수 없음 (훅들은 App이 불려오면서 실행되는데, useGoogleLogin은 <GoogleOAuthProvider/> 안에 속해야 하기 때문에 외부에서 선언한 다음 App에서 호출해야함
export default function LoginButtion({ backend, setAuthorized, fetchSchedules }: { backend: string; setAuthorized: Dispatch<SetStateAction<boolean | undefined>>; fetchSchedules: () => void }) {
    const redirect_uri = import.meta.env.VITE_API_ADDRESS;

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const code = params.get('code');

        if (code) {
            (async () => {
                // Unauthorized 에러 대신 로딩 창 띄우기
                setAuthorized(undefined);

                // code 노출 억제
                window.history.replaceState({}, document.title, window.location.pathname);
                await axios.post(backend + "/auth/getToken", { auth_code: code, redirect_uri }).then((res) => {
                    if (res.status == 200) {
                        localStorage.setItem("calisync_token", res.data);
                        fetchSchedules();
                        setAuthorized(true);
                    }
                }).catch(() => {
                    setAuthorized(false);
                });
            })()
        }
    });

    const login = useGoogleLogin({
        flow: 'auth-code',
        ux_mode: "redirect",
        scope: 'openid email profile',
        redirect_uri
    });

    return (
        <button className={"font-suite text-white border border-neutral-500 p-2 scale-70"} onPointerUp={() => login()}>
            <span className={"font-bold"}>Google 계정</span>
            <span className={"text-neutral-300"}>으로 인증하기</span>
        </button>
    )
}