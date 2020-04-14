import styled from "styled-components";
export default styled.a`
    margin-right: 10px;
    margin-left: 20px;
    padding-right: 32px;
    text-align: left;
    
    border-top: rgb(21, 21, 21) 3px solid;
    font-family: 'Red Hat Display', sans-serif;
    font-size: 18px;
    position: relative;
    box-sizing: border-box;
    cursor: pointer;
    outline: none;
    text-decoration: none;
    padding: 17px 16px 20px 16px;
    white-space: nowrap;
    text-align: left;
    color: white;
    display: inline-block;
    
    &:hover {
        text-shadow: 0 0 5px #ffffff;
        color: white;
        background-color: rgb(21, 21, 21);
        transition: text-shadow 0.5s;
    }
`