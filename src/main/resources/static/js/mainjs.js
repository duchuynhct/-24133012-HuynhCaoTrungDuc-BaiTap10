$(document).ready(function() {
    // Hiển thị thông tin người dùng đăng nhập thành công
    $.ajax({
        type: 'GET',
        url: '/users/me',
        dataType: 'json',
        contentType: "application/json; charset=utf-8",
        beforeSend: function(xhr) {
            if (localStorage.token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.token);
            }
        },
        success: function(data) {
            var json = JSON.stringify(data, null, 4);
            // $('#profile').html(json);
            $('#profile').html('Xin chào: ' + data.fullName + ' (' + data.email + ')');
            if (data.images) {
                $('#images').attr('src', data.images).show();
            }
            // console.log("SUCCESS : ", data);
        },
        error: function(e) {
            var json = e.responseText;
            $('#feedback').html("Chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
            $('#profile').html("Bạn chưa đăng nhập!");
            // console.log("ERROR : ", e);
        }
    });

    // Hàm đăng xuất
    $('#logout').click(function() {
        localStorage.clear();
        window.location.href = "/login";
    });

    // Hàm Login
    $('#Login').click(function() {
        var email = document.getElementById('email').value;
        var password = document.getElementById('password').value;
        var basicInfo = JSON.stringify({
            email: email,
            password: password
        });

        $.ajax({
            type: "POST",
            url: "/auth/login",
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            data: basicInfo,
            success: function(data) {
                localStorage.token = data.token;
                // alert('Got a token from the server! Token: ' + data.token);
                window.location.href = "/user/profile";
            },
            error: function(xhr) {
                var msg = "Login Failed";
                if (xhr.responseJSON && xhr.responseJSON.description) {
                    msg = xhr.responseJSON.description;
                }
                alert(msg);
            }
        });
    });
});
