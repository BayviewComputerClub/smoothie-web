import React from "react";

import DesktopHeader from "./DesktopHeader";
import HeaderTitle from "./components/HeaderTitle";
import HeaderItem from "./components/HeaderItem";
import HeaderRight from "./components/HeaderRight";

export default class Header extends React.Component {
    constructor(props) {
        super(props);

    }

    render() {
        return (
            <DesktopHeader>
                <HeaderTitle href={"/"}>smoothie-web</HeaderTitle>
                <HeaderItem href={"/problems"}>Problems</HeaderItem>
                <HeaderItem href={"/contests"}>Contests</HeaderItem>
                <HeaderItem href={"/rankings"}>Ranking</HeaderItem>
                {this.props.isAdmin ? <HeaderItem href={"/admin"}>Admin </HeaderItem> : null}
                {
                    this.props.isAuth ?
                        <HeaderRight>
                            <HeaderItem href={"/user/" + this.props.username}>Hello, {this.props.username}!</HeaderItem>
                            <HeaderItem href={"/logout"}>Logout</HeaderItem>
                        </HeaderRight>
                        :
                        <HeaderRight>
                            <HeaderItem href={"/login"}>Login</HeaderItem>
                            <HeaderItem href={"/register"}>Register</HeaderItem>
                        </HeaderRight>
                }
            </DesktopHeader>
        );
    }

}