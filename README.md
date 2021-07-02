# LogiSmart

> 융합캡스톤디자인 강의에서 진행한 프로젝트

## 📆 개발 기간

2021.03. - 2021.06.

## 📚 사용 기술

### 웹개발

- HTML / CSS
- Java Script
- Java
- JSP
- Apache Tomcat
- MySQL

### 안드로이드 앱 개발

- Arduino
- Android
- Java
- Bluetooth
- Firebase

## 1. 개요

온도관리 IoT 콜드체인 시스템.  
블루투스를 통해 물류의 위치와 온도 데이터를 실시간으로 전송하고,  
적정 온도 범위를 벗어나기 전에 관리자에게 미리 알려주는 시스템을 구축한다.

## 2. 팀 구성 및 역할

### 2-1. 팀원

웹 개발 1명  
안드로이드 앱 개발 1명 🙋‍♀️

### 2-2. 역할

- 아두이노 프로그래밍
  - 블루투스 모듈을 통해 온습도와 GPS 센서값을 앱으로 전송
- 안드로이드 앱 개발
  - 로그인 기능과 본인인증 기능 개발
  - 앱에서 아두이노를 블루투스로 연결하여 데이터 수신
  - 서버와 HTTP 통신
  - Firebase Cloud Messaging으로 경고 Push 전송

## 3. 안드로이드 앱

### 3-1. 관리자용

<div>
<img src="https://user-images.githubusercontent.com/81695614/124213342-5751fe00-db2b-11eb-95e3-055050f61432.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124213404-72bd0900-db2b-11eb-9e60-b79daa10a463.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124213412-78b2ea00-db2b-11eb-9802-688f8d05e525.jpg" width="30%" height="30%"/>
</div>

(로그인 화면 / 관리자 초기 화면 / 운반품 선택 화면)  
관리자는 로그인 후에 목록 버튼을 클릭하여 운반품 목록을 볼 수 있다.  
목록은 담당한 물품만 출력되며, 운반품을 선택하여 현재 상황을 볼 수 있다.

<div>
<img src="https://user-images.githubusercontent.com/81695614/124215206-89b12a80-db2e-11eb-865a-912082e99262.gif" width="100%" height="100%">
</div>

(운반품 선택 과정)

### 3-2. 운반자용

<div>
<img src="https://user-images.githubusercontent.com/81695614/124213894-51105180-db2c-11eb-9440-f0603c17850b.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124213902-540b4200-db2c-11eb-81c1-a96c788a1ce0.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124213905-55d50580-db2c-11eb-877c-2e67b00a8b2c.jpg" width="30%" height="30%"/>
</div>

(본인 인증 화면 / 정보 작성 화면 / 수락 대기 화면)  
Firebase Phone Authentication을 사용해 본인 인증을 구현했다.  
본인 인증 후에 정보 작성을 하면 서버로 데이터가 전달된다.  
전달된 데이터를 토대로 관리자가 수락을 하면 수락 대기 화면을 벗어나 블루투스 연결 화면으로 이동한다.

<div>
<img src="https://user-images.githubusercontent.com/81695614/124215892-eb25c900-db2f-11eb-8072-83130cef7a1b.gif" width="30%" height="30%">
</div>

(본인 인증 과정)

<div>
<img src="https://user-images.githubusercontent.com/81695614/124213958-700ee380-db2c-11eb-9475-1be83c4b8412.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124213971-74d39780-db2c-11eb-84c5-8d383184c900.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124213978-769d5b00-db2c-11eb-8074-ad1ecab7abc2.jpg" width="30%" height="30%"/>
</div>

(운반 초기 화면 / 블루투스 화면 / 위치 퍼미션 수락 화면)  
블루투스 연결을 위한 블루투스와 위치 퍼미션에 대한 수락창을 구현했다.

<div>
<img src="https://user-images.githubusercontent.com/81695614/124214013-85840d80-db2c-11eb-90f1-7a1f315ded98.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124214016-874dd100-db2c-11eb-9aed-41827fde5493.jpg" width="30%" height="30%"/>
</div>

(위치 서비스 화면 / 블루투스 목록 화면)  
위치 서비스를 켜면 서버에서 전달된 블루투스 기기명을 토대로 연결 가능한 블루투스 목록을 출력한다.

<div>
<img src="https://user-images.githubusercontent.com/81695614/124213106-f7f3ee00-db2a-11eb-9e10-cac1ef49ef44.gif" width="100%" height="100%">
</div>

(기기 연결 후 데이터 전달 및 백그라운드 작동)

<div>
<img src="https://user-images.githubusercontent.com/81695614/124215287-b2392480-db2e-11eb-9f32-c4323949c236.jpg" width="30%" height="30%"/>
<img src="https://user-images.githubusercontent.com/81695614/124215291-b402e800-db2e-11eb-878b-256eefbd4bdc.jpg" width="30%" height="30%"/>
</div>

(경고 알림 / 위험 알림)  
물품이 적정 온도 범위 경계에 근접하면 경고 알림을, 범위를 넘어서면 위험 알림을 보낸다.  
Firebase Cloud Messaging을 사용하여 서버에서 Push 알림을 보냈다.
