import styled from "styled-components";
export default styled.a`
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
    transition: color, background-color 0.3s, text-shadow 0.3s, box-shadow 0.3s, border-top;
    
    &:hover {
        text-shadow: 0 0 5px #e0e0e0;
        color: rgb(21, 21, 21);
        border-top: #737679 3px solid;
        background-color: white;
        box-shadow: 0 4px 8px -1px white;
        transition: color 0.3s, background-color 0.3s, text-shadow 0.3s, box-shadow 0.3s;
    }
`